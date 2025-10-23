package co.vinni.kafka.SBConsumidor.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservaListeners {

    // Servicio 1: Procesamiento de Asientos
    @RabbitListener(queues = "reservas.queue")
    public void procesarReservaAsientos(String mensaje) {
        System.out.println("🎬 [SERVICIO ASIENTOS] Procesando reserva: " + mensaje);

        // Extraer información del mensaje
        String[] partes = mensaje.split("\\|");
        if (partes.length > 1) {
            String usuario = partes[1].replace("Usuario:", "");
            String asientos = partes[4].replace("Asientos:", "");
            System.out.println("✅ [SERVICIO ASIENTOS] Asientos " + asientos + " confirmados para: " + usuario);
        }

        System.out.println("---");
    }

    // Servicio 2: Servicio de Notificaciones
    @RabbitListener(queues = "reservas.queue")
    public void enviarNotificacion(String mensaje) {
        System.out.println("📧 [SERVICIO NOTIFICACIONES] Enviando confirmación: " + mensaje);

        // Extraer información del mensaje
        String[] partes = mensaje.split("\\|");
        if (partes.length > 2) {
            String usuario = partes[1].replace("Usuario:", "");
            String pelicula = partes[2].replace("Pelicula:", "");
            System.out.println("✅ [SERVICIO NOTIFICACIONES] Email enviado a: " + usuario + " para: " + pelicula);
        }

        System.out.println("---");
    }

    // Servicio 3: Servicio de Analytics
    @RabbitListener(queues = "reservas.queue")
    public void registrarMetricas(String mensaje) {
        System.out.println("📊 [SERVICIO ANALYTICS] Registrando métricas: " + mensaje);

        // Simular registro en base de datos
        System.out.println("✅ [SERVICIO ANALYTICS] Métricas actualizadas - Reserva contabilizada en estadísticas");
        System.out.println("---");
    }
}