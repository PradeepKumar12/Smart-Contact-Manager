package com.scm.deep.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.scm.deep.helper.MessageCenter;
import com.scm.deep.model.User;
import com.scm.deep.repository.UserRepository;

@Controller
public class HomeController {
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;
	
	@RequestMapping("/")
	public String home(Model m) {
	m.addAttribute("title","Home - Smart Contact Manager");
		return "home";
	}
	@RequestMapping("/about")
	public String about(Model m) {
	m.addAttribute("title","About - Smart Contact Manager");
		return "about";
	}
	
	@RequestMapping("/signup")
	public String signup(Model m) {
	m.addAttribute("title","Register - Smart Contact Manager");
	m.addAttribute("user", new User());
		return "signup";
	}
	
	//handler for registering user
	@RequestMapping(value="/do_register",method = RequestMethod.POST)
	public String registerUser(@Valid @ModelAttribute("user") User user,BindingResult br,@RequestParam(value = "agreement",defaultValue = "false")boolean agreement,Model model,HttpSession session)
	{
		try {
			if(!agreement) {
				System.out.print("you have not agrred the terms and conditions");
				throw new Exception("you have not agrred the terms and conditions");
			}
			if(br.hasErrors())
			{
				System.out.print("error:"+br.toString());
				model.addAttribute("user",user);
				return "signup";
			}
		
			user.setRole("ROLE_USER");
			user.setImageUrl("contact_profile.png");
			user.setEnabled(true);
			user.setPassword(passwordEncoder.encode(user.getPassword()));
//			System.out.print(agreement);
//			System.out.print(user);
			User result = this.userRepository.save(user);
			model.addAttribute("User",new User());
			session.setAttribute("message", new MessageCenter("Successfully Registered!!","alert-success"));
			return "signup";
		}catch(Exception e) {
			e.printStackTrace();
			model.addAttribute("User",user);
			session.setAttribute("message", new MessageCenter("Something went wrong!!"+e.getMessage(),"alert-danger"));
			return "signup";
		}
		
	}
	
	//handler for custom login
	@GetMapping("/signin")
	public String customLogin(Model model)
	{
		model.addAttribute("title", "Login-Smart Contact Manager");
		return "login";
	}
	
	
}
