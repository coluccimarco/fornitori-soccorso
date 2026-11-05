package it.exprivia.fornitori.service;

/** Pubblicato dentro la transazione: l'assegnazione parte solo se e quando questa ha fatto commit. */
public record MissionAssignmentRequested(Long missionId) {
}
