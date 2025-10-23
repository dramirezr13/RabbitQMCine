package co.vinni.kafka.SBConsumidor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SbConsumidorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SbConsumidorApplication.class, args);
        System.out.println("✅ Consumidor RabbitMQ iniciado - Escuchando mensajes...");
    }
}