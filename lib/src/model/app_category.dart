enum AppCategory {
  game('Game'),
  audio('Audio'),
  video('Video'),
  image('Image'),
  social('Social'),
  news('News'),
  maps('Maps'),
  productivity('Productivity'),
  undefined('Undefined');

  final String name;
  const AppCategory(this.name);
}
