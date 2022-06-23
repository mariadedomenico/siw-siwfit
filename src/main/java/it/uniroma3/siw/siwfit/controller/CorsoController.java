package it.uniroma3.siw.siwfit.controller;

import java.io.IOException;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.util.FileUploadUtil;

import it.uniroma3.siw.siwfit.controller.validator.CorsoValidator;
import it.uniroma3.siw.siwfit.model.Corso;
import it.uniroma3.siw.siwfit.model.User;
import it.uniroma3.siw.siwfit.service.CategoriaService;
import it.uniroma3.siw.siwfit.service.CorsoService;
import it.uniroma3.siw.siwfit.service.TrainerService;
import it.uniroma3.siw.siwfit.service.UserService;

@Controller
public class CorsoController {

	@Autowired
	private CorsoService corsoService;

	@Autowired
	private CorsoValidator corsoValidator;

	@Autowired
	private UserService userService;

	@Autowired
	private TrainerService trainerService;
	
	@Autowired
	private CategoriaService categoriaService;

	/* id è del corso.
	 * Il metodo resituisce un corso tramite il suo id.
	 */
	@GetMapping("/user/corso/{id}/{idU}")
	public String getCorso(@PathVariable("id")Long id, @PathVariable("idU") Long idU, Model model) {
		Corso corso = corsoService.findById(id);
		model.addAttribute("corso", corso);
		User user = this.userService.findById(idU);
		model.addAttribute("user", user);
		Boolean prenotazione = (user.getCorsiPrenotati().contains(corso)) || (corso.getIscritti().size() >= corso.getNumeroMaxPersone());
		Boolean cancellazione = (user.getCorsiPrenotati().contains(corso));
		int disponibilita = corso.getNumeroMaxPersone() - corso.getIscritti().size();
		model.addAttribute("disponibilita", disponibilita);
		model.addAttribute("prenotazione", prenotazione);
		model.addAttribute("cancellazione", cancellazione);
		return "user/corso.html";
	}

	@GetMapping("/user/prenota/{id}/{idU}") //id del corso
	public String getPrenota(@PathVariable("id") Long id, @PathVariable("idU") Long idU, Model model) {
		User user = this.userService.findById(idU);
		Corso corso = this.corsoService.findById(id);
		user.getCorsiPrenotati().add(corso);
		this.userService.save(user);
		corso.getIscritti().add(user);
		this.corsoService.save(corso);
		model.addAttribute("user",user);
		model.addAttribute("corso", corso);
		Boolean prenotazione = (user.getCorsiPrenotati().contains(corso)) || (corso.getIscritti().size() >= corso.getNumeroMaxPersone());
		Boolean cancellazione = (user.getCorsiPrenotati().contains(corso));
		int disponibilita = corso.getNumeroMaxPersone() - corso.getIscritti().size();
		model.addAttribute("disponibilita", disponibilita);
		model.addAttribute("prenotazione", prenotazione);
		model.addAttribute("cancellazione", cancellazione);
		return "redirect:/user/corso/{id}/{idU}";
	}

	@GetMapping("/user/delete_corsoPrenotato/{id}/{idU}")
	public String deleteCorsoPrenotatoFromCorso(@PathVariable("id")Long id, @PathVariable("idU") Long idU, Model model) {
		User user = this.userService.findById(idU);
		Corso corso = this.corsoService.findById(id);
		user.getCorsiPrenotati().remove(corso);
		corso.getIscritti().remove(user);
		this.corsoService.save(corso);
		this.userService.save(user);

		Boolean prenotazione = (user.getCorsiPrenotati().contains(corso)) || (corso.getIscritti().size() >= corso.getNumeroMaxPersone());
		Boolean cancellazione = (user.getCorsiPrenotati().contains(corso));
		model.addAttribute("prenotazione", prenotazione);
		model.addAttribute("cancellazione", cancellazione);
		model.addAttribute("user", user);
		model.addAttribute("corso", corso);
		int disponibilita = corso.getNumeroMaxPersone() - corso.getIscritti().size();
		model.addAttribute("disponibilita", disponibilita);
		return "user/corso.html";
	}

	@GetMapping("/admin/corsi")
	public String getCategorieAdmin(Model model) {
		model.addAttribute("corsi", corsoService.findAll());
		return "admin/corso/corsi.html";
	}

	@GetMapping("/admin/crea_corso")
	public String addCorsoForm(Model model) {
		model.addAttribute("trainers", this.trainerService.findAll());
		model.addAttribute("categorie", this.categoriaService.findAll());
		model.addAttribute("corso", new Corso());
		return "admin/corso/crea_corso.html";
	}

	@PostMapping("/admin/new_corso") 
	public String addCorso(@Valid @ModelAttribute("corso") Corso corso, @RequestParam("image") MultipartFile multipartFile, BindingResult bindingResult, Model model) throws IOException {		
		this.corsoValidator.validate(corso, bindingResult);
		if(!bindingResult.hasErrors()) {
			
        	/*UPLOAD FOTO*/
        	String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());     //prende il nome del file 
            corso.setImg("/images/" + fileName);     //e lo concatena con /images
            this.corsoService.save(corso);
            String uploadDir = "src/main/resources/static/images/";        
            if(fileName != null && multipartFile != null && !fileName.isEmpty())
            	FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);         //carica la foto nel percorso specificato
			
			model.addAttribute("corso", corso);
			return "redirect:/admin/corsi";   
		}
		else {
			return "admin/corso/crea_corso.html";
		}
	}

	@GetMapping("/admin/modifica_corso/{id}")
	public String modificaCorsoForm(@PathVariable("id")  Long id, Model model) {
		model.addAttribute("trainers", this.trainerService.findAll());
		model.addAttribute("categorie", this.categoriaService.findAll());
		model.addAttribute("corso", this.corsoService.findById(id));
		return "admin/corso/modifica_corso.html";
	}
	
	@PostMapping("/admin/edit_corso/{id}") 
	public String modificaCorso(@PathVariable("id")  Long id, @Valid @ModelAttribute("corso") Corso corso, @RequestParam("image") MultipartFile multipartFile, BindingResult bindingResult, Model model) throws IOException {		
		this.corsoValidator.validate(corso, bindingResult);
		if (!bindingResult.hasErrors()) { 
			
        	/*UPLOAD FOTO*/
        	String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
            String uploadDir = "src/main/resources/static/images/";
            if(fileName != null && multipartFile != null && !fileName.isEmpty()) {
            	FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
                corso.setImg("/images/" + fileName);
            }
            else {
            	corso.setImg(this.corsoService.findById(id).getImg());
            }
            this.corsoService.save(corso);		
			model.addAttribute("corso", corso);
			return "redirect:/admin/corsi";
		} 
		else {
			model.addAttribute("trainers", this.trainerService.findAll());
			model.addAttribute("categorie", this.categoriaService.findAll());
			return "admin/corso/modifica_corso.html"; 
		}
	}
	
	@GetMapping("/admin/delete_corso/{id}")
	public String deleteCorso(@PathVariable Long id) {
		this.corsoService.deleteById(id);
		return "redirect:/admin/corsi";
	}
	
	@GetMapping("/admin/dettagli_corso/{id}")
	public String getDettagliCorso(@PathVariable Long id, Model model) {
		model.addAttribute("corso", this.corsoService.findById(id));
		model.addAttribute("corsi", this.corsoService.findAll());
		return "admin/corso/dettagli_corso";
	}


}