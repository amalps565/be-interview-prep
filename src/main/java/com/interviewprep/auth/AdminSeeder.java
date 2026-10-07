package com.interviewprep.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

  private final AdminProperties admin;
  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;

  public AdminSeeder(
      AdminProperties admin, AppUserRepository users, PasswordEncoder passwordEncoder) {
    this.admin = admin;
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!admin.isConfigured()) {
      log.info("ADMIN_EMAIL and ADMIN_PASSWORD are not set; no admin account was created");
      return;
    }
    String email = AuthService.normalise(admin.email());
    if (users.existsByEmail(email)) {
      return;
    }
    users.save(new AppUser(email, passwordEncoder.encode(admin.password()), Role.ADMIN));
    log.info("Created the admin account from ADMIN_EMAIL");
  }
}
