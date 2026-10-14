package org.dentalcrm.web.agente.engine;

import org.dentalcrm.web.agente.AgenteConversacionalService;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado estructurado de un paso (§18): mensaje + flujo + paso + entrada
 * esperada + opciones + si el flujo terminó.
 *
 * <p>El {@link FlowEngine} persiste este estado en el contexto después de que
 * cada {@link org.dentalcrm.web.agente.flujo.Flujo} lo devuelve.
 */
public class PasoResultado {

    private String mensaje;
    private String flujo;
    private String paso;
    private String expectedInput;
    private List<OpcionMenu> opciones;
    private MenuActual menuActual;
    private boolean completado;
    private boolean transferirHumano;
    private AgenteConversacionalService.Adjunto adjunto;
    /** El usuario no se entendió: suma a {@code intentosFallidos} (§14). */
    private boolean fallo;
    /** El flujo no supo interpretarlo: el motor continúa con la intención general. */
    private boolean ignorado;
    /** Si es falso, el motor deja el contexto tal cual estaba. */
    private boolean persistir = true;
    /** El flujo pide retomar el que estaba suspendido (registro o identificación, §7). */
    private boolean retomar;
    /** El flujo cambió de código en el contexto: hay que volver a despachar. */
    private boolean reenviar;

    public static PasoResultado continuar(String mensaje) {
        PasoResultado r = new PasoResultado();
        r.mensaje = mensaje;
        return r;
    }

    public static PasoResultado continuar(String mensaje, String paso) {
        return continuar(mensaje).en(paso);
    }

    public static PasoResultado fin(String mensaje) {
        PasoResultado r = new PasoResultado();
        r.mensaje = mensaje;
        r.completado = true;
        r.flujo = null;
        r.paso = null;
        r.opciones = new ArrayList<>();
        r.menuActual = MenuActual.PRINCIPAL;
        return r;
    }

    /** Terminó y debe retomarse el flujo suspendido. */
    public static PasoResultado retomando() {
        PasoResultado r = new PasoResultado();
        r.retomar = true;
        return r;
    }

    public static PasoResultado ignorado() {
        PasoResultado r = new PasoResultado();
        r.ignorado = true;
        r.persistir = false;
        return r;
    }

    public PasoResultado en(String paso) {
        this.paso = paso;
        return this;
    }

    public PasoResultado flujo(String flujo) {
        this.flujo = flujo;
        return this;
    }

    public PasoResultado esperado(String expectedInput) {
        this.expectedInput = expectedInput;
        return this;
    }

    public PasoResultado menu(MenuActual menu) {
        this.menuActual = menu;
        return this;
    }

    public PasoResultado opciones(List<OpcionMenu> opciones) {
        this.opciones = opciones;
        return this;
    }

    public PasoResultado fallo() {
        this.fallo = true;
        return this;
    }

    public PasoResultado transferir() {
        this.transferirHumano = true;
        return this;
    }

    public PasoResultado adjunto(AgenteConversacionalService.Adjunto adjunto) {
        this.adjunto = adjunto;
        return this;
    }

    public String mensaje() {
        return mensaje;
    }

    public String flujo() {
        return flujo;
    }

    public String paso() {
        return paso;
    }

    public String expectedInput() {
        return expectedInput;
    }

    public List<OpcionMenu> opciones() {
        return opciones;
    }

    public MenuActual menuActual() {
        return menuActual;
    }

    public boolean completado() {
        return completado;
    }

    public boolean transferirHumano() {
        return transferirHumano;
    }

    public AgenteConversacionalService.Adjunto adjunto() {
        return adjunto;
    }

    public boolean esFallo() {
        return fallo;
    }

    public boolean esIgnorado() {
        return ignorado;
    }

    public boolean persistir() {
        return persistir;
    }

    public boolean retomar() {
        return retomar;
    }

    /** Pasa el control a otro flujo dejando la entrada intacta (§2). */
    public static PasoResultado reenviar() {
        PasoResultado r = new PasoResultado();
        r.reenviar = true;
        return r;
    }

    /** Pasa el control y encadena un mensaje propio (p. ej. "Te registré..."). */
    public static PasoResultado reenviar(String prefijo) {
        PasoResultado r = reenviar();
        r.mensaje = prefijo;
        return r;
    }

    /** Antepone un mensaje al resultado final (registro que retoma un flujo, §7). */
    public void anteponer(String prefijo) {
        if (prefijo == null || prefijo.isBlank()) {
            return;
        }
        this.mensaje = this.mensaje == null || this.mensaje.isBlank()
                ? prefijo : prefijo + "\n\n" + this.mensaje;
    }

    public boolean esReenviar() {
        return reenviar;
    }
}
