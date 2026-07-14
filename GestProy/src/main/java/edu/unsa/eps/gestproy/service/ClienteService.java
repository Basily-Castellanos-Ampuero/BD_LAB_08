package edu.unsa.eps.gestproy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.unsa.eps.gestproy.dao.ClienteDao;
import edu.unsa.eps.gestproy.model.Cliente;

@Service
public class ClienteService {

    private final ClienteDao dao;

    public ClienteService(ClienteDao dao) {
        this.dao = dao;
    }

    public List<Cliente> listar()            { return dao.listar(); }
    public Cliente buscar(int cod)           { return dao.buscar(cod); }
    public void adicionar(Cliente c)         { dao.mantener("ADICIONAR", c); }
    public void modificar(Cliente c)         { dao.mantener("MODIFICAR", c); }
    public void cambiarEstado(int cod, String operacion) { dao.cambiarEstado(cod, operacion); }
}
