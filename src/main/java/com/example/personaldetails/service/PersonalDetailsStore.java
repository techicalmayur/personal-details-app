package com.example.personaldetails.service;

import com.example.personaldetails.model.PersonalDetails;
import com.example.personaldetails.model.PersonalDetailsRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds one visitor's records. Because this bean is session-scoped, Spring keeps one
 * instance per HTTP session (it is stored as a session attribute). No database involved:
 * the data disappears when the session expires or the server restarts.
 */
@Component
@SessionScope
public class PersonalDetailsStore implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Keeps a single visitor from filling the memory of a 512 MB free instance. */
    static final int MAX_ENTRIES = 50;

    private final Map<String, PersonalDetails> items = new LinkedHashMap<>();

    public synchronized List<PersonalDetails> findAll() {
        return List.copyOf(items.values());
    }

    public synchronized PersonalDetails findById(String id) {
        PersonalDetails found = items.get(id);
        if (found == null) {
            throw new PersonNotFoundException(id);
        }
        return found;
    }

    public synchronized PersonalDetails create(PersonalDetailsRequest request) {
        if (items.size() >= MAX_ENTRIES) {
            throw new StoreFullException(MAX_ENTRIES);
        }
        PersonalDetails created = PersonalDetails.of(UUID.randomUUID().toString(), request);
        items.put(created.id(), created);
        return created;
    }

    public synchronized PersonalDetails update(String id, PersonalDetailsRequest request) {
        if (!items.containsKey(id)) {
            throw new PersonNotFoundException(id);
        }
        PersonalDetails updated = PersonalDetails.of(id, request);
        items.put(id, updated);
        return updated;
    }

    public synchronized void delete(String id) {
        if (items.remove(id) == null) {
            throw new PersonNotFoundException(id);
        }
    }
}
