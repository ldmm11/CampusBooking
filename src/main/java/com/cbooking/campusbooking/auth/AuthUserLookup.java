package com.cbooking.campusbooking.auth;

import java.util.Optional;

/** Implemented by the user persistence module (part A). Never returns password hashes. */
public interface AuthUserLookup {
    Optional<AuthUser> findById(long id);
}
