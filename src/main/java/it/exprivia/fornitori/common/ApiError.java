package it.exprivia.fornitori.common;

/** Stable public error envelope; never contains exception messages or SQL. */
public record ApiError(int status, String error) {
}
