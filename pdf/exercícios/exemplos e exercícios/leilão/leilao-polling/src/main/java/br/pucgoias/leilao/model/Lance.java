package br.pucgoias.leilao.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Lance {

    private String usuario;
    private double valor;
    private String horario;

    public Lance() {}

    public Lance(String usuario, double valor) {
        this.usuario = usuario;
        this.valor = valor;
        this.horario = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }
}
