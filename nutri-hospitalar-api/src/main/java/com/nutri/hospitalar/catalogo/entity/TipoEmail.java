package com.nutri.hospitalar.catalogo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_email")
@Getter
@Setter
@NoArgsConstructor
public class TipoEmail extends CatalogoEntity {}
