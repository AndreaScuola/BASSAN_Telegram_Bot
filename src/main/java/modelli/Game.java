package modelli;

import java.util.List;

public class Game {
    public int id;
    public String name;
    public String released;
    public double rating;
    public int metacritic;
    public String background_image;
    public List<PlatformWrapper> platforms;
    public List<Genre> genres;
    public List<Publisher> publishers;
}
