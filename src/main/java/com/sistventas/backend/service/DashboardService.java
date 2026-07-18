package com.sistventas.backend.service;

import com.sistventas.backend.dto.DashboardResumenDto;
import com.sistventas.backend.security.UserPrincipal;

public interface DashboardService {
    DashboardResumenDto resumen(UserPrincipal principal);
}
