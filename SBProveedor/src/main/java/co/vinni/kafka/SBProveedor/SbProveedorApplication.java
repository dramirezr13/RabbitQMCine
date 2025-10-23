package co.vinni.kafka.SBProveedor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@SpringBootApplication
public class SbProveedorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SbProveedorApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(RabbitTemplate rabbitTemplate){
        return args -> {
            // Mensaje inicial para probar que RabbitMQ funciona
            rabbitTemplate.convertAndSend("direct.exchange", "reserva.direct",
                    "Sistema de cine iniciado - Listo para recibir reservas");
            System.out.println("✅ Mensaje inicial enviado a RabbitMQ: Sistema de cine listo!");
        };
    }
}