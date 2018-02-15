package sa.devming.picturewidget.database;

public class PictureData {
    private int widgetId;
    private int position;
    private String imageUri;
    private String isLoad;

    public PictureData(int widgetId, int position, String imageUri) {
        this.widgetId = widgetId;
        this.position  = position;
        this.imageUri = imageUri;
        this.isLoad = "N";
    }

    public int getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(int widgetId) {
        this.widgetId = widgetId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getIsLoad() {
        return isLoad;
    }

    public void setIsLoad(String isLoad) {
        this.isLoad = isLoad;
    }
}
