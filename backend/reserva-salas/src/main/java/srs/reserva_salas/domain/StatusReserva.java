package srs.reserva_salas.domain;

import java.util.List;

public enum StatusReserva {
    CONFIRMADA, EM_USO, CONCLUIDA, CANCELADA_PELO_USUARIO, CANCELADA_POR_AUSENCIA;

    /** Status que ocupam a sala (RN001 e RN006). */
    public static final List<StatusReserva> BLOQUEANTES = List.of(CONFIRMADA, EM_USO);
}
