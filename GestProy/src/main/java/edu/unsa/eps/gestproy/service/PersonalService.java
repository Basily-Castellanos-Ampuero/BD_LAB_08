package edu.unsa.eps.gestproy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.unsa.eps.gestproy.dao.PersonalDao;
import edu.unsa.eps.gestproy.model.PerCar;
import edu.unsa.eps.gestproy.model.Personal;

@Service
public class PersonalService {

    private final PersonalDao dao;

    public PersonalService(PersonalDao dao) {
        this.dao = dao;
    }

    public List<Personal> listar()            { return dao.listar(); }
    public Personal buscar(int cod)           { return dao.buscar(cod); }
    public void adicionar(Personal p)         { dao.mantener("ADICIONAR", p); }
    public void modificar(Personal p)         { dao.mantener("MODIFICAR", p); }
    public void cambiarEstado(int cod, String operacion) { dao.cambiarEstado(cod, operacion); }

    public List<PerCar> listarCargos(int perCod) { return dao.listarCargos(perCod); }
    public void adicionarCargo(int perCod, int carProCod) { dao.mantenerCargo("ADICIONAR", perCod, carProCod); }
    public void cambiarEstadoCargo(int perCod, int carProCod, String operacion) {
        dao.mantenerCargo(operacion, perCod, carProCod);
    }
}
