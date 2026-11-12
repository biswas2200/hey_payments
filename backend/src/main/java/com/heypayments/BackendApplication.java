package com.heypayments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Main application entrypoint for the Hey Payments backend service. */
@SpringBootApplication
public class BackendApplication {

  /**
   * Application entry point.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}
