import 'dart:io';

import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/material.dart';
import 'package:camera/camera.dart';
import 'dart:async';
import 'package:dio/dio.dart';
import 'package:video_player/video_player.dart';
import 'package:intl/intl.dart';
import 'Media.dart';
import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:chewie/chewie.dart';

Future<void> main() async {
  cameras = await availableCameras();
  runApp(MyApp());
}

final GlobalKey<ScaffoldState> _key = GlobalKey<ScaffoldState>();
List<CameraDescription> cameras;
Map<String, Media> filesList = {};
StreamController<Map<String, Media>> _stream;


class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Kolorblind'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);
  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}


class _MyHomePageState extends State<MyHomePage> {
  bool isFilesBody;
  int _currentIndex = 0;
  final List<Widget> _children = [
    CameraBody(),
    FilesBody(),
  ];

  void onTabTapped(int index) {
    setState(() {
      _currentIndex = index;
      if(index == 1) isFilesBody = true;
      else isFilesBody = false;
    });
  }

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    isFilesBody = false;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _key,
      backgroundColor: Colors.grey[100],
      appBar: AppBar(
        title: Text(widget.title),
        actions: <Widget>[
          isFilesBody ? IconButton(
            icon: Icon(Icons.refresh),
            onPressed: () {
              List imageIds = [];
              Map<String, dynamic> imageJson = {};
              List videoIds = [];
              Map<String, dynamic> videoJson = {};
              filesList.forEach((id,media) {
                if(media.type == "image") imageIds.add(id);
                else videoIds.add(id);
              });

              if (imageIds.isNotEmpty) {
                imageJson.putIfAbsent("list", () => imageIds);

                Dio dio = new Dio();
                dio.post("http://192.168.160.73:8080/api/image/infoList",data: imageJson, options: Options(
                  method: 'POST', contentType: ContentType.json, responseType: ResponseType.json,))
                  .then((response) {
                    List info = response.data;

                    info.forEach((media) {
                      Media newFile = new Media.fromJsonImage(media);
                      filesList.update(newFile.id, (Media) => newFile);
                    });

                    _stream.add(filesList);
                  })
                  .catchError((error) => print(error));
              }

              if (videoIds.isNotEmpty) {
                videoJson.putIfAbsent("list", () => videoIds);

                Dio dio = new Dio();
                dio.post("http://192.168.160.73:8080/api/video/infoList",data: videoJson, options: Options(
                  method: 'GET', contentType: ContentType.json, responseType: ResponseType.json,))
                  .then((response) {
                  List info = response.data;

                  info.forEach((media) {
                    Media newFile = new Media.fromJsonVideo(media);
                    filesList.update(newFile.id, (Media) => newFile);
                  });

                  _stream.add(filesList);
                  })
                  .catchError((error) => print(error));
              }
            },
          )
          : Container(),
        ],
      ),
      body: _children[_currentIndex],
      bottomNavigationBar: BottomNavigationBar(

        onTap: onTabTapped,
        currentIndex: _currentIndex,
        items: [
          BottomNavigationBarItem(
            icon: Icon(Icons.camera_alt),
            title: Text("Camera"),
          ),
          BottomNavigationBarItem(
            title: Text("Library"),
            icon: Icon(Icons.photo_library),
          ),
        ],
      ),
    );
  }
}

void updateMediaInfo() {
  List imageIds = [];
  Map<String, dynamic> imageJson = {};
  List videoIds = [];
  Map<String, dynamic> videoJson = {};
  filesList.forEach((id,media) {
    if(media.type == "image") imageIds.add(id);
    else videoIds.add(id);
  });

  if (imageIds.isNotEmpty) {
    imageJson.putIfAbsent("list", () => imageIds);

    Dio dio = new Dio();
    dio.post("http://192.168.160.73:8080/api/image/infoList",data: imageJson, options: Options(
      method: 'POST', contentType: ContentType.json, responseType: ResponseType.json,))
        .then((response) {
          List info = response.data;

          info.forEach((media) {
            Media newFile = new Media.fromJsonImage(media);
            filesList.update(newFile.id, (Media) => newFile);
          });

          _stream.add(filesList);
    })
        .catchError((error) => print(error));
  }

  if (videoIds.isNotEmpty) {
    videoJson.putIfAbsent("list", () => videoIds);

    Dio dio = new Dio();
    dio.post("http://192.168.160.73:8080/api/video/infoList",data: videoJson, options: Options( method: 'POST', contentType: ContentType.json, responseType: ResponseType.json,))
      .then((response) {
        List info = response.data;

        info.forEach((media) {
          Media newFile = new Media.fromJsonVideo(media);
          filesList.update(newFile.id, (Media) => newFile);
        });

        _stream.add(filesList);
      })
      .catchError((error) => print(error));
  }
}

class CameraBody extends StatefulWidget {
  @override
  _CameraBodyState createState() => _CameraBodyState();
}

class _CameraBodyState extends State<CameraBody> {
  CameraController controller;
  String imagePath;
  String videoPath;
  IconData isRecordingIcon;
  VideoPlayerController videoController;
  VoidCallback videoPlayerListener;

  String timestamp() => DateTime.now().millisecondsSinceEpoch.toString();

  void logError(String code, String message) => print('Error: $code\nError Message: $message');

  void showInSnackBar(String message) {
    _key.currentState.showSnackBar(SnackBar(content: Text(message)));
  }

  Future<String> takePicture() async {
    if (!controller.value.isInitialized) {
      showInSnackBar('Error: select a camera first.');
      return null;
    }
    final Directory extDir = await getApplicationDocumentsDirectory();
    final String dirPath = '${extDir.path}/Pictures/';
    await Directory(dirPath).create(recursive: true);
    final String filePath = '$dirPath/${timestamp()}.jpg';

    if (controller.value.isTakingPicture) {
      // A capture is already pending, do nothing.
      return null;
    }

    try {
      await controller.takePicture(filePath);
    } on CameraException catch (e) {
      _showCameraException(e);
      return null;
    }
    return filePath;
  }

  Future<String> startVideoRecording() async {
    if (!controller.value.isInitialized) {
      showInSnackBar('Error: select a camera first.');
      return null;
    }

    final Directory extDir = await getApplicationDocumentsDirectory();
    final String dirPath = '${extDir.path}/Movies/';
    await Directory(dirPath).create(recursive: true);
    final String filePath = '$dirPath/${timestamp()}.mp4';

    if (controller.value.isRecordingVideo) {
      // A recording is already started, do nothing.
      return null;
    }

    try {
      videoPath = filePath;
      await controller.startVideoRecording(filePath);
    } on CameraException catch (e) {
      _showCameraException(e);
      return null;
    }
    return filePath;
  }

  Future<bool> stopVideoRecording() async {
    if (!controller.value.isRecordingVideo) {
      return false;
    }

    try {
      await controller.stopVideoRecording();
    } on CameraException catch (e) {
      _showCameraException(e);
      return false;
    }

    await _startVideoPlayer();

    return true;
  }

  Future<void> _startVideoPlayer() async {
    final VideoPlayerController vcontroller =
    VideoPlayerController.file(File(videoPath));
    videoPlayerListener = () {
      if (videoController != null && videoController.value.size != null) {
        // Refreshing the state to update video player with the correct ratio.
        if (mounted) setState(() {});
        videoController.removeListener(videoPlayerListener);
      }
    };
    vcontroller.addListener(videoPlayerListener);
    await vcontroller.setLooping(true);
    await vcontroller.initialize();
    await videoController?.dispose();
    if (mounted) {
      setState(() {
        imagePath = null;
        videoController = vcontroller;
      });
    }
    await vcontroller.play();
  }

  void _showCameraException(CameraException e) {
    logError(e.code, e.description);
    showInSnackBar('Error: ${e.code}\n${e.description}');
  }

  @override
  void initState() {
    super.initState();
    isRecordingIcon = Icons.videocam;
    controller = CameraController(cameras[0], ResolutionPreset.high);
    controller.initialize().then((_) {
      if (!mounted) {
        return;
      }
      setState(() {});
    });
  }

  @override
  void dispose() {
    controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!controller.value.isInitialized) {
      return Container();
    }
    return Column(
      children: <Widget>[
        AspectRatio(
          aspectRatio: controller.value.aspectRatio,
          child: CameraPreview(controller)
        ),
        Padding(
          padding: const EdgeInsets.only(top: 30.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: <Widget>[
              IconButton(icon: Icon(isRecordingIcon,size: 40.0), onPressed: () {
                if(isRecordingIcon == Icons.videocam) {
                  startVideoRecording().then((String filePath) {
                    if (filePath != null) {
                      videoPath = filePath;
                      setState(() {
                        isRecordingIcon = Icons.stop;
                      });
                    }
                  });
                }

                else {
                  stopVideoRecording().then((bool isGood) {
                    if (isGood) {
                      setState(() {
                        isRecordingIcon = Icons.videocam;
                      });
                      showDialog(
                          context: context,
                          builder: (BuildContext context) {
                            return SimpleDialog(
                              children: <Widget>[
                                videoController.value.initialized ? AspectRatio(aspectRatio: videoController.value.aspectRatio, child: VideoPlayer(videoController),) : Container(),
                                Text("Send this video to filter?"),
                                Row(
                                  children: <Widget>[
                                    FlatButton(child: Text("No"), onPressed: () {videoController.pause();Navigator.of(context).pop();},),
                                    FlatButton(child: Text("Yes"), onPressed: () {
                                      videoController.pause();
                                      Navigator.of(context).pop();
                                      Dio dio = new Dio();
                                      FormData formdata = new FormData(); // just like JS
                                      formdata.add("file", new UploadFileInfo(File(videoPath), basename(videoPath),contentType: ContentType.parse("video/mp4")));
                                      dio.post("http://192.168.160.73:8080/api/video/upload", data: formdata, options: Options(
                                        method: 'POST', contentType: ContentType.json))
                                          .then((response) {
                                            Media newFile = new Media.fromJsonVideo(response.data);
                                            print(newFile == null);
                                            filesList.putIfAbsent(newFile.id, () => newFile);
                                            print(filesList);
                                            _stream.add(filesList);
                                          })
                                          .catchError((error) => print(error));
                                    },),
                                  ],
                                ),
                              ],
                            );
                          }
                      );
                    }
                  });
                }
              }),
              IconButton(icon: Icon(Icons.photo_camera,size: 40.0,), onPressed: () {
                takePicture().then((String filePath) {
                  if (mounted) {
                    setState(() {
                      imagePath = filePath;
                      //videoController?.dispose();
                      //videoController = null;
                    });
                    if (filePath != null) {
                      print("hey");
                      showDialog(
                        context: context,
                        builder: (BuildContext context) {
                          return SimpleDialog(
                            children: <Widget>[

                              Image.file(File(imagePath)),

                              Text("Send this image to filter?"),
                              Row(
                                children: <Widget>[
                                  FlatButton(child: Text("No"), onPressed: () {Navigator.of(context).pop();},),
                                  FlatButton(child: Text("Yes"), onPressed: () {
                                    Dio dio = new Dio();
                                    FormData formdata = new FormData(); // just like JS
                                    formdata.add("file", new UploadFileInfo(File(filePath), basename(filePath), contentType: ContentType.parse("image/jpeg")));
                                    dio.post("http://192.168.160.73:8080/api/image/upload", data: formdata, options: Options(
                                    method: 'POST',contentType: ContentType.json))
                                        .then((response) {
                                          Media newFile = new Media.fromJsonImage(response.data);
                                          print(newFile == null);
                                          filesList.putIfAbsent(newFile.id, () => newFile);
                                          print(filesList);
                                          _stream.add(filesList);
                                        })
                                        .catchError((error) => print(error));
                                  }),
                                ]
                              ),
                            ]
                          );
                        }
                      );
                    }
                  }
                });
              }),

              IconButton(icon: Icon(Icons.live_tv,size: 40.0), onPressed: null,),
            ],
          ),
        )
      ],
    );
  }
}

class FilesBody extends StatefulWidget {
  @override
  _FilesBodyState createState() => _FilesBodyState();
}

class _FilesBodyState extends State<FilesBody> {

  Image img;


  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    _stream = new StreamController<Map<String, Media>>();
    _stream.add(filesList);
    print(filesList);

  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 5, 20, 5),
      child: StreamBuilder<Map<String, Media>>(
        stream: _stream.stream,
        builder: (context, snapshot) {
          if (snapshot.hasError) {
            return Text(snapshot.error);
          }

          if (snapshot.hasData) {
            return ListView.builder(
                itemCount: snapshot.data.length,
                itemBuilder: (context, int index) {
                  String key = snapshot.data.keys.elementAt(index);
                  Media media = snapshot.data[key];
                  DateTime date = new DateTime.fromMillisecondsSinceEpoch(media.date*1000,isUtc: true).toLocal();
                  return Card(
                      child: Padding(
                          padding: const EdgeInsets.all(15.0),
                          child: Column(
                            children: <Widget>[
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: <Widget>[
                                  Text(media.id,style: TextStyle(fontSize: 16.0,fontWeight: FontWeight.bold),),
                                  Text(media.status,style: TextStyle(fontSize: 16.0,fontWeight: FontWeight.bold),),
                                ],
                              ),
                              Divider(),
                              Padding(
                                padding: const EdgeInsets.only(top: 5.0,bottom: 10.0),
                                child: Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                                  children: <Widget>[
                                    Text(media.type),
                                    Text(DateFormat.yMd().add_jm().format(date)),
                                  ],
                                ),
                              ),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceAround,
                                children: <Widget>[
                                  Text(media.fileSize.toInt().toString() + " bits"),
                                  media.processTime == 0.0 ? Text("-") : Text(media.processTime.toString() + " seconds"),
                                ],
                              ),

                              media.status == "READY" ? DownloadImage(context, media.id, media.type == "image") : Container(),

                            ],
                          )
                      )
                  );
                }
            );
          }

          if (snapshot.connectionState != ConnectionState.done) {
            return Center(
              child: CircularProgressIndicator(),
            );
          }
        }
      )
    );
  }
}

Widget DownloadImage(BuildContext context, String id, bool isImage){

  if(isImage){
    return Column(
      children: <Widget>[
        Padding(padding: EdgeInsets.only(top: 10.0)),
        Divider(),
        RaisedButton(
          onPressed: () {
            Dio dio = new Dio();
            dio.get("http://192.168.160.73:8080/api/image/download?id=" + id, options: Options(
                method: 'GET', responseType: ResponseType.bytes))
                .then((response) {
                  if(response.statusCode == 200) {
                    Navigator.push(
                        context,
                        MaterialPageRoute(builder: (context) => ShowImage(response.data))
                    );
                  }
                })
                .catchError((error) => print(error));
          },
          color: Colors.blue,
          child: Text("Download and Show",style: TextStyle(color: Colors.white),),
        )
      ],
    );
  }
  else{
    return Column(
      children: <Widget>[
        Padding(padding: EdgeInsets.only(top: 10.0)),
        Divider(),
        RaisedButton(
          onPressed: () {
            Dio dio = new Dio();
            dio.get("http://192.168.160.73:8080/api/video/download?id=" + id, options: Options(
                method: 'GET',responseType: ResponseType.bytes))
                .then((response) {
                  if(response.statusCode == 200) {
                    Navigator.push(
                        context,
                        MaterialPageRoute(builder: (context) => ShowVideo(response.data))
                    );
                  }
                })

                .catchError((error) => print(error));
          },
          color: Colors.blue,
          child: Text("Download and Show",style: TextStyle(color: Colors.white),),
        )
      ],
    );
  }
}



class ShowVideo extends StatefulWidget {
  List<int> video;

  ShowVideo(this.video);

  @override
  _ShowVideoState createState() => _ShowVideoState(video);
}

class _ShowVideoState extends State<ShowVideo> {

  List<int> video;

  _ShowVideoState(this.video);

  VideoPlayerController videoController;
  VoidCallback videoPlayerListener;
  ChewieController chewieController;

  Future<void> _startVideoPlayer() async {

    final Directory extDir = await getApplicationDocumentsDirectory();
    final String dirPath = '${extDir.path}/Uploads/';
    await Directory(dirPath).create(recursive: true);
    final String filePath = dirPath + 'video.mp4';

    File file = new File(filePath);

    await file.writeAsBytes(video);

    final VideoPlayerController vcontroller =  VideoPlayerController.file(file);

     chewieController = ChewieController(
      videoPlayerController: vcontroller,
       aspectRatio: 3 / 4,
       allowFullScreen: false,
       looping: true,
       autoPlay: true,
    );
  }

  @override
  void initState(){
    // TODO: implement initState
    super.initState();
  }

  @override
  void dispose() {
    super.dispose();
    videoController.dispose();
    chewieController.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: <Widget>[
          Center(
            child: FutureBuilder<void>(
              future: _startVideoPlayer(), // a previously-obtained Future<String> or null
              builder: (BuildContext context, AsyncSnapshot<void> snapshot) {
                switch (snapshot.connectionState){
                  case ConnectionState.none:
                    // TODO: Handle this case.
                    break;
                  case ConnectionState.waiting:
                    // TODO: Handle this case.
                    break;
                  case ConnectionState.active:
                    // TODO: Handle this case.
                    break;
                  case ConnectionState.done:
                    return Chewie(controller: chewieController,);
                }
                return Container();
              },
            )
          ),
          Positioned(
            top: 30,
            left: 10,
            child: IconButton(
                icon: Icon(Icons.close,color: Colors.white,size: 25.0,),
                onPressed: () {chewieController.pause(); Navigator.pop(context);}
            ),
          ),
        ],
      ),

    );
  }
}


class ShowImage extends StatelessWidget{
  List<int> img;

  ShowImage(this.img);

  @override
  Widget build(BuildContext context) {


    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: <Widget>[
          Center(
            child:  Image.memory(
              Uint8List.fromList(img),
            ),
          ),
          Positioned(
            top: 30,
            left: 10,
            child: IconButton(
                icon: Icon(Icons.close,color: Colors.white,size: 25.0,),
                onPressed: () => Navigator.pop(context)
            ),
          ),
        ],
      ),

    );
  }

}
