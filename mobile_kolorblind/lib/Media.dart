class Media {
  String id;
  int date;
  String fullName;
  double fileSize;
  String status;
  String type;
  double processTime;

  Media.fromJsonImage( Map<String, dynamic> json ) :
      id = json["id"],
      date = json["createDate"],
      fullName = json["fullName"],
      fileSize = json["fileSize"],
      status = json["status"],
      type = "image",
      processTime = json["processTime"];

  Media.fromJsonVideo( Map<String, dynamic> json ) :
      id = json["id"],
      date = json["createDate"],
      fullName = json["fullName"],
      fileSize = json["fileSize"],
      status = json["status"],
      type = "video",
      processTime = json["processTime"];
}