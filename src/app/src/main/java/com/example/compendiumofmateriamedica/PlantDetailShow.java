package com.example.compendiumofmateriamedica;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

import model.Datastructure.Plant;
import model.Datastructure.PlantTreeManager;
import model.Datastructure.RBTreeNode;


/**
 * @author: Haochen Gong, Hongjun Xu
 * @uid: u , u7733037
 * @datetime: 2024/05/16
 * @description: Display detailed introduction interface of plants
 **/
public class PlantDetailShow extends AppCompatActivity {

    private TextView commonName;
    private TextView slug;
    private TextView scientificName;
    private TextView genus;
    private TextView family;
    private TextView description;
    private int plantId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail_show);

        commonName = findViewById(R.id.common_name);
        slug = findViewById(R.id.slug);
        scientificName = findViewById(R.id.scientific_name);
        genus = findViewById(R.id.genus);
        family = findViewById(R.id.family);
        description = findViewById(R.id.description);

        // get plant id from last activity
        plantId = (int) this.getIntent().getSerializableExtra("PlantId");
        ArrayList<Plant> plants = PlantTreeManager.getInstance().search(PlantTreeManager.PlantInfoType.ID, String.valueOf(plantId));
        Plant plant = plants.get(0);


        commonName.setText(plant.getCommonName());
        slug.setText(plant.getSlug());
        scientificName.setText(plant.getScientificName());
        genus.setText(plant.getGenus());
        family.setText(plant.getFamily());
        description.setText(plant.getDescription());
    }


}