package com.heypayments.auth.domain.service;

public interface PasswordHasher {

  String hashPassword(String rawPassword);

  boolean verifyPassword(String rawPassword, String hashedPassword);

  String hashPin(String rawPin);

  boolean verifyPin(String rawPin, String hashedPin);
}
