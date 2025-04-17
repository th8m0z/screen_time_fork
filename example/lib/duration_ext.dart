extension DurationExt on Duration {
  String get inString {
    final hours = inHours;
    final minutes = inMinutes.remainder(60);
    final seconds = inSeconds.remainder(60);

    final parts = <String>[];
    if (hours > 0) parts.add('$hours hour${hours != 1 ? 's' : ''}');
    if (minutes > 0) parts.add('$minutes minute${minutes != 1 ? 's' : ''}');
    if (seconds > 0 || parts.isEmpty) {
      parts.add('$seconds second${seconds != 1 ? 's' : ''}');
    }

    return parts.join(' ');
  }
}
