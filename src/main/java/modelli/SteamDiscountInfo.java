package modelli;

public class SteamDiscountInfo {
    public String name;
    public int originalPrice;
    public int salePrice;
    public int discountPercent;

    public SteamDiscountInfo(String name, int originalPrice, int salePrice, int discountPercent) {
        this.name = name;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.discountPercent = discountPercent;
    }
}