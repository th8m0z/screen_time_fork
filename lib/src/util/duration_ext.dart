extension ScrenTimeDurationExt on Duration {
  int get hour => inHours;
  int get minute => inMinutes % 60;
  int get second => inSeconds % 60;
  int get millisecond => inMilliseconds % 1000;
}
