package com.example.personaldetails.web;

import com.example.personaldetails.model.PersonalDetails;
import com.example.personaldetails.model.PersonalDetailsRequest;
import com.example.personaldetails.service.PersonalDetailsStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/people")
public class PersonalDetailsController {

    private final PersonalDetailsStore store; // session-scoped proxy: resolves per request

    public PersonalDetailsController(PersonalDetailsStore store) {
        this.store = store;
    }

    @GetMapping
    public List<PersonalDetails> list() {
        return store.findAll();
    }

    @GetMapping("/{id}")
    public PersonalDetails get(@PathVariable String id) {
        return store.findById(id);
    }

    @PostMapping
    public ResponseEntity<PersonalDetails> create(@Valid @RequestBody PersonalDetailsRequest request) {
        PersonalDetails created = store.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public PersonalDetails update(@PathVariable String id, @Valid @RequestBody PersonalDetailsRequest request) {
        return store.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        store.delete(id);
        return ResponseEntity.noContent().build();
    }
}
