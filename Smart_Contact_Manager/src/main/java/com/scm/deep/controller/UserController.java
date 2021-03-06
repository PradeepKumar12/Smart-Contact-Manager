package com.scm.deep.controller;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.scm.deep.helper.MessageCenter;
import com.scm.deep.model.Contact;
import com.scm.deep.model.User;
import com.scm.deep.repository.ContactRepository;
import com.scm.deep.repository.UserRepository;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
//		System.out.println("username:" + userName);
		// get user using userName(email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println("user:" + user);
		model.addAttribute("user", user);
	}

	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// open add from handler
	@GetMapping("/addContact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@Valid @ModelAttribute Contact contact,BindingResult br, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session,Model model) {
		System.out.println("Contact :- "+contact);

		try {
				if (br.hasErrors()) {
				System.out.println("Error" + br.toString());
				model.addAttribute("contact", contact);
				return "normal/add_contact_form";
			}
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			// processing and uploading file
			if (file.isEmpty()) {
				// if the file is empty then try our message
				System.out.println("File is empty!!!!!");
				contact.setImage("contact.png");
			} else {
				// upload the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Image is uploaded....!!!!!");
			}

			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepository.save(user);

//			System.out.println("DATA:" + contact);
			System.out.println("Added to database");
			// message success......
			session.setAttribute("message",
					new MessageCenter("Your Contact is Successfully added!!! Add More...", "success"));
			return "normal/add_contact_form";

		} catch (Exception e) {
			System.out.println("ERROR" + e.getMessage());
			e.printStackTrace();
			// error message
			session.setAttribute("message", new MessageCenter("Something went wrong !! Try Again....", "danger"));
			return "normal/add_contact_form";
		}
		
	}

	
	// show contacts handler
	// per page=5[n]
	// current page=0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer currentPage, Model m, Principal principal) {
		m.addAttribute("title", "Show User Contact");
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		int page=currentPage-1;
		Pageable pageable = PageRequest.of(page, 8);
		Page<Contact> contacts = this.contactRepository.findcontactByUser(user.getId(), pageable);
		int count = user.getContacts().size();
//		System.out.println("count ->"+user.getContacts().size());
		System.out.println("Contacts: "+contacts);
		m.addAttribute("count", count);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		return "normal/show_contacts";
	}

	// showing particular contact details
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
//		System.out.println("CID" + cId);

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		return "normal/contact_detail";

	}

	// delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, Principal principal,
			HttpSession session) {
		Contact contact = this.contactRepository.findById(cId).get();
//		System.out.println("Contact " + contact.getcId());
		// contact.setUser(null);
		// String userName = principal.getName();
		// User user = this.userRepository.getUserByUserName(userName);

		/*
		 * if(user.getId()==contact.getUser().getId()) {
		 * this.contactRepository.delete(contact); System.out.println("deleted");
		 * session.setAttribute("message", new
		 * Message("Contact deleted successfully...","success"));
		 * 
		 * }
		 */
		// this.contactRepository.delete(contact);
		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		this.userRepository.save(user);

		System.out.println("deleted");
		session.setAttribute("message", new MessageCenter("Contact deleted successfully...", "success"));

		return "redirect:/user/show-contacts/1";

	}

	// open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {
		m.addAttribute("title", "Update contact");
		Contact contact = this.contactRepository.findById(cid).get();
		m.addAttribute("contact", contact);
		return "normal/update_form";
	}

	// update contact handler
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {

		try {
			// old contact details
			Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();
			// image
			if (!file.isEmpty()) {
				// file work
				// rewrite
				// delete old photo
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile, oldcontactDetail.getImage());
				file1.delete();

				// update new photo
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldcontactDetail.getImage());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new MessageCenter("Your contact is updated...", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("CONTACT NAME " + contact.getName());
		System.out.print("CONTACT ID " + contact.getcId());
		return "redirect:/user/" + contact.getcId() + "/contact";
	}

	// your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}

	// open settings handler
	@GetMapping("/settings")
	public String openSetting() {
		return "normal/settings";
	}

	// change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		System.out.println("OLD PASSWORD " + oldPassword);
		System.out.println("NEW PASSWORD " + newPassword);

		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());

		if (this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new MessageCenter("Your password is successfully changed...", "success"));

		} else {
			session.setAttribute("message", new MessageCenter("Please Enter correct old password !!!..", "danger"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}
}
