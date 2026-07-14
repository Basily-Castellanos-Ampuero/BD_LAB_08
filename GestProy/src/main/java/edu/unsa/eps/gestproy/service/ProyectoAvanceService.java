package edu.unsa.eps.gestproy.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import edu.unsa.eps.gestproy.dao.ProyectoAvanceDao;
import edu.unsa.eps.gestproy.model.ProyectoAvance;
import edu.unsa.eps.gestproy.model.ProyectoMovimiento;

@Service
public class ProyectoAvanceService {

    private final ProyectoAvanceDao dao;

    public ProyectoAvanceService(ProyectoAvanceDao dao) {
        this.dao = dao;
    }

    public ProyectoAvance resumen(int cli, int tip, int sec) {
        return dao.resumen(cli, tip, sec);
    }

    public List<ProyectoMovimiento> movimientos(int cli, int tip, int sec) {
        return dao.movimientos(cli, tip, sec);
    }

    public int registrar(int cli, int tip, int sec, int perCod, int carProCod,
                         int etpCod, LocalDate fecReg, int horTra, int minTra) {
        return dao.registrar(cli, tip, sec, perCod, carProCod, etpCod, fecReg, horTra, minTra);
    }
}
