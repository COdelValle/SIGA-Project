package cl.siga.msusuariosauth.integration.graph;

/**
 * Resultado del aprovisionamiento en Entra ID: oid resuelto o creado y clave
 * temporal generada (nunca se registra en logs).
 */
public record ProvisionedUser(String oid, String temporaryPassword) {
}
