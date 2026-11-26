package model;

import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Estacion {

    private Integer id;
    private Integer localidadId;
    private Integer empresaId;
    private String tipo;
    private String codigoPostal;
    private String direccion;
    private String margen;
    private Double latitud;
    private Double longitud;
    private LocalDateTime fechaActualizacion;
    private String horario;
    private String tipoVenta;
    private String remision;
    private String tipoServicio;

}
