package edu.unsa.eps.gestproy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.unsa.eps.gestproy.dao.ProyectoDao;
import edu.unsa.eps.gestproy.dao.ProyectoEquipoDao;
import edu.unsa.eps.gestproy.model.PersonalDisponible;
import edu.unsa.eps.gestproy.model.Proyecto;
import edu.unsa.eps.gestproy.model.ProyectoEquipoItem;

@Service
public class ProyectoService {

    private final ProyectoDao dao;
    private final ProyectoEquipoDao equipoDao;

    public ProyectoService(ProyectoDao dao, ProyectoEquipoDao equipoDao) {
        this.dao = dao;
        this.equipoDao = equipoDao;
    }

    public List<Proyecto> listar()                          { return dao.listar(); }
    public Proyecto buscar(int cli, int tip, int sec)       { return dao.buscar(cli, tip, sec); }
    public int crear(Proyecto p)                            { return dao.crear(p); }
    public void editar(Proyecto p)                          { dao.editar(p); }
    public void cambiarEstado(int cli, int tip, int sec, String estado) {
        dao.cambiarEstado(cli, tip, sec, estado);
    }

    public List<ProyectoEquipoItem> equipo(int cli, int tip, int sec) {
        return equipoDao.listar(cli, tip, sec);
    }

    public List<PersonalDisponible> disponibles(int cli, int tip, int sec) {
        return equipoDao.disponibles(cli, tip, sec);
    }

    public void asignarEquipo(int cli, int tip, int sec, int perCod, int carProCod) {
        equipoDao.asignar(cli, tip, sec, perCod, carProCod);
    }

    public void quitarEquipo(int cli, int tip, int sec, int perCod, int carProCod) {
        equipoDao.quitar(cli, tip, sec, perCod, carProCod);
    }

    public void reactivarEquipo(int cli, int tip, int sec, int perCod, int carProCod) {
        equipoDao.reactivar(cli, tip, sec, perCod, carProCod);
    }
}
