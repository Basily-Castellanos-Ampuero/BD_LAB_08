package edu.unsa.eps.gestproy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.unsa.eps.gestproy.dao.ReferencialDao;
import edu.unsa.eps.gestproy.model.referencial.ReferencialTabla;
import edu.unsa.eps.gestproy.model.referencial.RegistroReferencial;

@Service
public class ReferencialService {

    private final ReferencialDao dao;

    public ReferencialService(ReferencialDao dao) {
        this.dao = dao;
    }

    public List<RegistroReferencial> listar(ReferencialTabla tabla) {
        return dao.listar(tabla);
    }

    public RegistroReferencial buscar(ReferencialTabla tabla, String cod) {
        return dao.buscar(tabla, cod);
    }

    public void adicionar(ReferencialTabla tabla, RegistroReferencial r) {
        dao.mantener(tabla, "ADICIONAR", r);
    }

    public void modificar(ReferencialTabla tabla, RegistroReferencial r) {
        dao.mantener(tabla, "MODIFICAR", r);
    }

    public void cambiarEstado(ReferencialTabla tabla, String cod, String operacion) {
        RegistroReferencial r = new RegistroReferencial();
        r.setCod(cod);
        dao.mantener(tabla, operacion, r);
    }
}
