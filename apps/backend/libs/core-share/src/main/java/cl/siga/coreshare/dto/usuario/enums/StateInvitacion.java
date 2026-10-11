package cl.siga.coreshare.dto.usuario.enums;

/**
 * Estado de una invitación de usuario. La invitación se crea con el correo
 * (UPN de Entra ID) y el rol; al primer inicio de sesión del invitado se
 * vincula con su {@code oid} y pasa a {@link #VINCULADA}.
 */
public enum StateInvitacion {
    INVITADO,
    VINCULADA
}
