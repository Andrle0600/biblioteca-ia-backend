package com.biblioteca.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biblioteca.model.Ubicacion;
import com.biblioteca.service.UbicacionService;

@CrossOrigin(origins = "https://andrle0600.github.io", allowCredentials = "true")
@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionApiController {

    @Autowired
    private UbicacionService ubicacionService;

    @GetMapping
    public ResponseEntity<List<Ubicacion>> obtenerUbicaciones() {
        return ResponseEntity.ok(ubicacionService.listarUbicaciones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ubicacion> obtenerUbicacion(@PathVariable Long id) {
        return ubicacionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Ubicacion> crearUbicacion(@RequestBody Ubicacion ubicacion) {
        Ubicacion guardada = ubicacionService.guardarUbicacion(ubicacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ubicacion> actualizarUbicacion(@PathVariable Long id, @RequestBody Ubicacion ubicacion) {
        if (ubicacion.getId() == null || !ubicacion.getId().equals(id)) {
            return ResponseEntity.badRequest().build();
        }
        if (ubicacionService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Ubicacion actualizada = ubicacionService.guardarUbicacion(ubicacion);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUbicacion(@PathVariable Long id) {
        if (ubicacionService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ubicacionService.eliminarUbicacion(id);
        return ResponseEntity.noContent().build();
    }
}
