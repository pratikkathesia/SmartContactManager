package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ContactRepository contactRepository;

// Method for adding common data to response
    @ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        String userName = principal.getName();
        System.out.println("USERNAME " + userName);

// Get the user using username(Email)	
        User user = userRepository.getUserByUserName(userName);
        System.out.println("USER " + user);
        model.addAttribute("user", user);
    }

// Home Dashboard
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("title", "User Dashboard");
        return "normal/user_dashboard";
    }

// Open add form controller 
    @GetMapping("/add-contact")
    public String OpenAddContactForm(Model model) {
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

// Processing Add Contact Form
    @PostMapping("/process-contact")
    public String processContact(       
    		@ModelAttribute Contact contact,
    		@RequestParam("profileImage") MultipartFile file,  
    		Principal principal,
    		HttpSession session
    ) 
    {
        try {
            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);
            
// Processing and uploading file
            if(file.isEmpty())
            {
            	System.out.println("File is empty");
            	contact.setImage("contact.png");
            }
            else
            {
            	contact.setImage(file.getOriginalFilename());
            	File saveFile = new ClassPathResource("static/img").getFile();
            	Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
            	Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            	System.out.println("Image Uploaded ");
            }
      
            contact.setUser(user);
            user.getContacts().add(contact);
            this.userRepository.save(user);

            System.out.println("DATA " + contact);
            System.out.println("Added to Database ");
            
// Print Success Message
            session.setAttribute("message",  new Message ("Your contact is added!", "success"));             
            
        } catch (Exception e) {
            System.out.println("ERROR" + e.getMessage());
            e.printStackTrace();
            
// Error Message
            session.setAttribute("message",  new Message ("Something went wrong!", "danger"));
        }

        return "normal/add_contact_form";
    }

// Handler to show contacts
    @GetMapping("/show-contacts/{page}")
    public String showContacts(@PathVariable("page") Integer page,Model m, Principal principal) {
    	m.addAttribute("title", "Show User Contacts");
    	
    	String userName = principal.getName();
    	User user = this.userRepository.getUserByUserName(userName);
    	
    	//Needs - current page
    	//Needs - contacts per page
    	PageRequest pageable = PageRequest.of(page, 3);
    	Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
    	m.addAttribute("contacts", contacts);
    	m.addAttribute("currentPage", page);
    	m.addAttribute("totalPages", contacts.getTotalPages());
    	return "normal/show_contacts";
    }

// Showing particular contact details
    
    @RequestMapping("/contact/{cId}")
    public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal)
    {
    	System.out.println("CID" + cId);
    	
    	Optional<Contact> contactOptional = this.contactRepository.findById(cId);
    	Contact contact = contactOptional.get();
    	
    	String userName = principal.getName();
    	User user = this.userRepository.getUserByUserName(userName);
    	
    	
    	if(user.getId()==contact.getUser().getId()) 
    	{
    		model.addAttribute("contact", contact);
    		model.addAttribute("title", contact.getName());
    	}
    	
    	return "normal/contact_detail";
    }
    
// Delete contact handler
    @GetMapping("/delete/{cId}") 
    public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session, Principal principal)    
    {
    	
    	Contact contact = this.contactRepository.findById(cId).get();

    	User user = this.userRepository.getUserByUserName(principal.getName());
    	user.getContacts().remove(contact);
    	this.userRepository.save(user);
    	System.out.println("Deleted");
    	session.setAttribute("message", new Message ("Contact deleted successfully", "success"));
    	return "redirect:/user/show-contacts/0";
    }
    
// Open Update page handler
    @PostMapping("/update-contact/{cId}")
    public String updateForm(@PathVariable("cId") Integer cId,Model m)
    {
    	m.addAttribute("title", "Update Contact");
    	Contact contact = this.contactRepository.findById(cId).get();
    	
    	m.addAttribute("contact", contact);
    	return "normal/update_form";
    }
    
// Update contact handler
    @PostMapping("/process-update")
    public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,Model m, HttpSession session, Principal principal)
    {
    	try
    	{
    		// Old Contact Details
    		Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
    		
    		
    		if(!file.isEmpty())
    		{
    			
// Delete Old Image
    			File deleteFile = new ClassPathResource("static/img").getFile();	
    			File file1 = new File(deleteFile, oldContactDetail.getImage());
    			file1.delete();
    			
// Update new image
    			
            File saveFile = new ClassPathResource("static/img").getFile();
            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            contact.setImage(file.getOriginalFilename());

    		}
    		else
    		{
    			contact.setImage(oldContactDetail.getImage());
    		}
    		User user = this.userRepository.getUserByUserName(principal.getName());
    		contact.setUser(user);
    		this.contactRepository.save(contact);
    		session.setAttribute("message", new Message("Your contact has been updated", "success"));
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	
    	System.out.println("Contact Name " + contact.getName());
    	System.out.println("Contact ID " + contact.getcId());
    	
    	return "redirect:/user/"+"contact/"+contact.getcId();
    }
    
// Your profile handler
    @GetMapping("/profile")
    public String yourProfile(Model model)
    {
    	model.addAttribute("title", "Profile page");
    	return "normal/profile";
    }
    
}