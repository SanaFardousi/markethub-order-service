package com.markethub.orders.domain.model;

import com.github.javaparser.quality.NotNull;

public record TransitionRequest(@NotNull OrderAction action) {}
