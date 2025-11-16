package ru.itmo.alfa.comand4.utils;

import ru.itmo.alfa.comand4.model.serial.ModelData;
import ru.itmo.alfa.comand4.service.ClusterProfiler;
import smile.clustering.KMeans;

import java.io.*;
import java.util.List;

public class ModelSerializer {

    public static void saveModel(KMeans model, List<String> vocabulary, ClusterProfiler clusterProfiler, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(model);
            oos.writeObject(vocabulary);
            oos.writeObject(clusterProfiler);
            System.out.println("Модель сохранена: " + filename);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    public static ModelData loadModel(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            KMeans model = (KMeans) ois.readObject();
            List<String> vocabulary = (List<String>) ois.readObject();
            ClusterProfiler clusterProfiler = (ClusterProfiler) ois.readObject();
            return new ModelData(model, vocabulary, clusterProfiler);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки: " + e.getMessage());
            return null;
        }
    }

}
