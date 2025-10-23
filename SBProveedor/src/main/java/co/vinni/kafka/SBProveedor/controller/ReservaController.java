package co.vinni.kafka.SBProveedor.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ReservaController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/")
    public String mostrarFormulario(Model model) {
        model.addAttribute("peliculas", new String[]{"Avengers: Endgame", "Spider-Man: No Way Home", "The Batman", "Avatar 2"});
        model.addAttribute("horarios", new String[]{"14:00", "17:00", "20:00", "22:30"});
        return "formulario-reserva";
    }

    @PostMapping("/reservar")
    public String procesarReserva(
            @RequestParam String nombre,
            @RequestParam String pelicula,
            @RequestParam String horario,
            @RequestParam String asientos,
            Model model) {

        // Crear mensaje de reserva
        String mensajeReserva = String.format(
                "RESERVA|Usuario:%s|Pelicula:%s|Horario:%s|Asientos:%s",
                nombre, pelicula, horario, asientos
        );

        // ENVIAR A LOS 3 EXCHANGES DE RABBITMQ
        // 1. Direct Exchange (Routing key específico)
        rabbitTemplate.convertAndSend("direct.exchange", "reserva.direct", mensajeReserva);

        // 2. Fanout Exchange (Sin routing key)
        rabbitTemplate.convertAndSend("fanout.exchange", "", mensajeReserva);

        // 3. Topic Exchange (Con patrón)
        String topicRoutingKey = "reservas." + pelicula.toLowerCase().replace(":", "").replace(" ", ".");
        rabbitTemplate.convertAndSend("topic.exchange", topicRoutingKey, mensajeReserva);

        // Mensaje de éxito
        model.addAttribute("mensaje", "✅ Reserva enviada! Mensaje publicado en 3 exchanges RabbitMQ");
        model.addAttribute("peliculas", new String[]{"Avengers: Endgame", "Spider-Man: No Way Home", "The Batman", "Avatar 2"});
        model.addAttribute("horarios", new String[]{"14:00", "17:00", "20:00", "22:30"});

        System.out.println("🎬 Reserva enviada a RabbitMQ: " + mensajeReserva);

        return "formulario-reserva";
    }
}