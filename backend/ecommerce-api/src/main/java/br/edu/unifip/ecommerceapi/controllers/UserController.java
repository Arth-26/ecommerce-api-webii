package br.edu.unifip.ecommerceapi.controllers;

import br.edu.unifip.ecommerceapi.dtos.AuthRequest;
import br.edu.unifip.ecommerceapi.dtos.UserDto;
import br.edu.unifip.ecommerceapi.models.Category;
import br.edu.unifip.ecommerceapi.models.Product;
import br.edu.unifip.ecommerceapi.models.User;
import br.edu.unifip.ecommerceapi.services.JwtService;
import br.edu.unifip.ecommerceapi.services.UserService;
import br.edu.unifip.ecommerceapi.utils.FileDownloadUtil;
import br.edu.unifip.ecommerceapi.utils.FileUploadUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    final UserService userService;

    final AuthenticationManager authenticationManager;

    final JwtService jwtService;

    public UserController(UserService userService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUserById(@PathVariable(value = "id") UUID id) {
        Optional<User> userOptional = userService.findById(id);
        return userOptional.<ResponseEntity<Object>>map(user -> ResponseEntity.status(HttpStatus.OK).body(user)).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found."));
    }

    @PostMapping("/register")
    public ResponseEntity<Object> saveUser(@Valid UserDto userDto, HttpServletRequest request) throws IOException {
        var user = new User();

        BeanUtils.copyProperties(userDto, user); // O que vai ser convertido para o quê vai ser convertido

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartRequest.getFile("image");

        if (multipartFile != null) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            String uploadDir = "user-images/";

            try {
                String filecode = FileUploadUtil.saveFile(fileName, uploadDir, multipartFile);
                user.setImage("/api/images/user-images/" + filecode);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Image not accepted.");
            }
        }

        // Salvar o usuário
        User savedUser = userService.save(user);

        // Gerar token de autenticação
        String token = jwtService.generateToken(savedUser.getUsername());

        // Retornar token junto com a resposta
        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> softDeleteUser(@PathVariable(value = "id") UUID id) {
        Optional<User> userOptional = userService.findById(id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        User instance = userOptional.get();

        // Verificar se o registro está ativo
        if (!instance.isActive()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category is not active.");
        }

        userService.softDelete(instance);
        return ResponseEntity.status(HttpStatus.OK).body("User deleted successfully.");
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> updateUser(@PathVariable(value = "id") UUID id, HttpServletRequest request) {
        Optional<User> userOptional = userService.findById(id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        User instance = userOptional.get();

        // Verificar se o registro está ativo
        if (!instance.isActive()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not active.");
        }

        Map<Object, Object> objectMap = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            objectMap.put(entry.getKey(), entry.getValue()[0]);
        }

        // Remover a chave "password" do objeto mapeado, se presente
        objectMap.remove("password");

        // Salvar a url da imagem em uma variável separada
        String imageUrl = null;
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartRequest.getFile("image");
        if (multipartFile != null) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            String uploadDir = "user-images/";

            try {
                String filecode = FileUploadUtil.saveFile(fileName, uploadDir, multipartFile);
                imageUrl = "/api/images/user-images/" + filecode;
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Image not accepted.");
            }
        }

        // Adicionar a url da imagem ao objeto mapeado, se ela foi enviada
        if (imageUrl != null) {
            objectMap.put("image", imageUrl);
        }

        userService.partialUpdate(instance, objectMap);

        return ResponseEntity.status(HttpStatus.OK).body(instance);
    }

    @GetMapping("/findByUsername")
    public ResponseEntity<Optional<User>> getUserByUsername(@Validated @RequestParam(value = "username") String username) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.findByUsername(username));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        if (authentication.isAuthenticated()) {
            String token = jwtService.generateToken(authRequest.getUsername());
            Map<String, String> response = new HashMap<String, String>();
            response.put("token", token);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user credentials!");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me(Authentication authentication) {
        // Obter o nome de usuário do usuário autenticado
        String username = authentication.getName();

        // Obter o usuário pelo nome de usuário
        Optional<User> userOptional = userService.findByUsername(username);
        return userOptional.<ResponseEntity<Object>>map(user -> ResponseEntity.status(HttpStatus.OK).body(user)).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found."));
    }

    @PatchMapping("/password")
    public ResponseEntity<Object> changePassword(Authentication authentication, @RequestBody Map<String, String> passwordMap) {
        // Verificar se as senhas fornecidas são iguais
        String newPassword = passwordMap.get("newPassword");
        String confirmPassword = passwordMap.get("confirmPassword");

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords do not match.");
        }

        // Obter o nome de usuário do usuário autenticado
        String username = authentication.getName();

        // Obter o usuário pelo nome de usuário
        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        // Atualizar a senha do usuário
        User user = userOptional.get();

        Map<Object, Object> objectMap = new HashMap<>();
        objectMap.put("password", newPassword);
        userService.partialUpdate(user, objectMap);

        // Gerar token de autenticação
        String token = jwtService.generateToken(user.getUsername());

        // Retornar token junto com a resposta
        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}