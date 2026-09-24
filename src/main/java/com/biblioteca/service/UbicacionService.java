package com.libros.reserva_libros.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.libros.reserva_libros.model.Ubicacion;
import com.libros.reserva_libros.repository.UbicacionRepository;

@Service
public class UbicacionService {

    @Autowired
    private UbicacionRepository ubicacionRepository;

    public List<Ubicacion> listarUbicaciones() {
        return ubicacionRepository.findAll();
    }

    public Optional<Ubicacion> buscarPorId(Long id) {
        return ubicacionRepository.findById(id);
    }

    public Ubicacion guardarUbicacion(Ubicacion ubicacion) {
        return ubicacionRepository.save(ubicacion);
    }

    public void eliminarUbicacion(Long id) {
        ubicacionRepository.deleteById(id);
    }
}
