package co.vinni.kafka.SBProveedor.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReservaController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // "Base de datos" en memoria para la demo
    private List<Reserva> reservas = new ArrayList<>();
    private Map<String, Set<String>> asientosOcupados = new HashMap<>();

    @GetMapping("/")
    public String mostrarFormulario(Model model) {
        model.addAttribute("peliculas", new String[]{
                "Avengers: Endgame", "Spider-Man: No Way Home",
                "The Batman", "Avatar 2", "Jurassic World"
        });
        model.addAttribute("horarios", new String[]{"14:00", "17:00", "20:00", "22:30"});

        // 📊 DATOS PARA EL DASHBOARD
        model.addAttribute("reservasHoy", reservas.size());
        model.addAttribute("peliculaPopular", calcularPeliculaPopular());
        model.addAttribute("proximaFuncion", "20:00 - Avengers");
        model.addAttribute("reservasRecientes", getReservasRecientes());

        return "formulario-reserva";
    }

    @PostMapping("/reservar")
    public String procesarReserva(
            @RequestParam String nombre,
            @RequestParam String pelicula,
            @RequestParam String horario,
            @RequestParam String asientos,
            Model model) {

        System.out.println("🎬 Procesando reserva para: " + nombre + " - " + pelicula);

        // 🔒 VERIFICAR ASIENTOS DISPONIBLES
        if (!sonAsientosDisponibles(pelicula, horario, asientos)) {
            model.addAttribute("error", "❌ Algunos asientos no están disponibles. Por favor verifica.");
            return mostrarFormulario(model);
        }

        // 📝 CONSTRUIR MENSAJE
        String mensajeReserva = String.format(
                "RESERVA|Usuario:%s|Pelicula:%s|Horario:%s|Asientos:%s",
                nombre, pelicula, horario, asientos
        );

        // 💾 GUARDAR RESERVA LOCALMENTE
        Reserva nuevaReserva = new Reserva(nombre, pelicula, horario, asientos);
        reservas.add(nuevaReserva);
        ocuparAsientos(pelicula, horario, asientos);

        System.out.println("✅ Reserva guardada localmente: " + nuevaReserva);

        // 🚀 ENVIAR A RABBITMQ - LOS 3 EXCHANGES
        try {
            // 1. DIRECT EXCHANGE
            rabbitTemplate.convertAndSend("direct.exchange", "reserva.direct", mensajeReserva);

            // 2. FANOUT EXCHANGE
            rabbitTemplate.convertAndSend("fanout.exchange", "", mensajeReserva);

            // 3. TOPIC EXCHANGE
            String topicRoutingKey = "reservas." + pelicula.toLowerCase()
                    .replace(":", "").replace(" ", ".").replace("-", "");
            rabbitTemplate.convertAndSend("topic.exchange", topicRoutingKey, mensajeReserva);

            System.out.println("📨 Mensaje enviado a los 3 exchanges de RabbitMQ");

        } catch (Exception e) {
            System.err.println("❌ Error al enviar a RabbitMQ: " + e.getMessage());
            model.addAttribute("error", "Error de comunicación con el sistema. Intenta nuevamente.");
            return mostrarFormulario(model);
        }

        model.addAttribute("mensaje", "✅ Reserva confirmada! Asientos: " + asientos +
                " - Mensaje enviado a RabbitMQ");
        return mostrarFormulario(model);
    }

    // 🆕 ENDPOINT PARA CONSULTAR ASIENTOS DISPONIBLES
    @GetMapping("/consultar-asientos")
    @ResponseBody
    public Map<String, Object> consultarAsientos(
            @RequestParam String pelicula,
            @RequestParam String horario) {

        System.out.println("🔍 Consultando asientos para: " + pelicula + " - " + horario);

        Map<String, Object> response = new HashMap<>();

        // Clave única para esta función
        String key = pelicula + "|" + horario;

        // Obtener asientos ocupados (o conjunto vacío si no hay)
        Set<String> ocupados = asientosOcupados.getOrDefault(key, new HashSet<>());

        System.out.println("Asientos ocupados para " + key + ": " + ocupados);

        // Generar asientos disponibles
        List<String> asientosDisponibles = generarAsientosDisponibles(ocupados);

        System.out.println("Total asientos disponibles: " + asientosDisponibles.size());

        response.put("disponibles", asientosDisponibles);
        response.put("ocupados", new ArrayList<>(ocupados));
        response.put("totalDisponibles", asientosDisponibles.size());

        return response;
    }

    // 🔧 MÉTODOS AUXILIARES

    /**
     * Verifica si los asientos solicitados están disponibles
     */
    private boolean sonAsientosDisponibles(String pelicula, String horario, String asientosSolicitados) {
        String key = pelicula + "|" + horario;
        Set<String> ocupados = asientosOcupados.getOrDefault(key, new HashSet<>());

        // Limpiar y dividir los asientos solicitados
        String[] asientosArray = asientosSolicitados.split(",");
        for (String asiento : asientosArray) {
            String asientoLimpio = asiento.trim().toUpperCase();
            if (ocupados.contains(asientoLimpio)) {
                System.out.println("❌ Asiento ocupado: " + asientoLimpio);
                return false;
            }
        }
        return true;
    }

    /**
     * Marca los asientos como ocupados
     */
    private void ocuparAsientos(String pelicula, String horario, String asientos) {
        String key = pelicula + "|" + horario;
        Set<String> ocupados = asientosOcupados.computeIfAbsent(key, k -> new HashSet<>());

        String[] asientosArray = asientos.split(",");
        for (String asiento : asientosArray) {
            String asientoLimpio = asiento.trim().toUpperCase();
            ocupados.add(asientoLimpio);
            System.out.println("📍 Asiento ocupado: " + asientoLimpio + " para " + key);
        }
    }

    /**
     * Genera lista de asientos disponibles
     */
    private List<String> generarAsientosDisponibles(Set<String> ocupados) {
        List<String> disponibles = new ArrayList<>();
        String[] filas = {"A", "B", "C"};
        int asientosPorFila = 5;

        for (String fila : filas) {
            for (int i = 1; i <= asientosPorFila; i++) {
                String asiento = fila + i;
                if (!ocupados.contains(asiento)) {
                    disponibles.add(asiento);
                }
            }
        }
        return disponibles;
    }

    /**
     * Calcula la película más popular basada en las reservas
     */
    private String calcularPeliculaPopular() {
        if (reservas.isEmpty()) {
            return "Ninguna";
        }

        Map<String, Integer> conteo = new HashMap<>();
        for (Reserva reserva : reservas) {
            String pelicula = reserva.getPelicula();
            conteo.put(pelicula, conteo.getOrDefault(pelicula, 0) + 1);
        }

        return conteo.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    /**
     * Obtiene las reservas más recientes (últimas 5)
     */
    private List<Reserva> getReservasRecientes() {
        return reservas.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * CLASE INTERNA PARA REPRESENTAR UNA RESERVA
     */
    public static class Reserva {
        private String usuario;
        private String pelicula;
        private String horario;
        private String asientos;
        private LocalDateTime timestamp;

        public Reserva(String usuario, String pelicula, String horario, String asientos) {
            this.usuario = usuario;
            this.pelicula = pelicula;
            this.horario = horario;
            this.asientos = asientos;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getUsuario() {
            return usuario;
        }

        public String getPelicula() {
            return pelicula;
        }

        public String getHorario() {
            return horario;
        }

        public String getAsientos() {
            return asientos;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("Reserva{usuario=%s, pelicula=%s, horario=%s, asientos=%s}",
                    usuario, pelicula, horario, asientos);
        }
    }
}