package hn.sinap.conciliacion.model;

public class AuthPayload {
    private int id;
    private String nombre;

    public AuthPayload(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Getters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
}
