package com.powersolutions.solarshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SolarShieldSubscriptionApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolarShieldSubscriptionApplication.class, args);
	}

    /*
    @Autowired
    private SquareSubscriptionCheckoutService checkoutService;
    private final ContactRepo contactRepo;
    private final SubscriptionRepo subscriptionRepo;

    public SolarShieldSubscriptionApplication(ContactRepo contactRepo,
                                              SubscriptionRepo subscriptionRepo) {
        this.contactRepo = contactRepo;
        this.subscriptionRepo = subscriptionRepo;
    }

	@Bean
	public CommandLineRunner demo() {
		return (args -> {

            subscriptionRepo.findById(1)
                    .ifPresent(sub -> contactRepo.findById(1).ifPresent(contact -> {;

                    SquareCheckoutResponse response = checkoutService.createSubscriptionPaymentLink(sub, contact);
                    System.out.println(response);

            }));

		});
	}
	*/


}
