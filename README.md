# Minesweeper-Solver 

A Minesweeper solver, written in Java 
 
This is a solver to solve a minesweeper game for you. I mean, it is too tedious to do it yourself, right? It works by taking a screenshot, analyze it, then moving the mouse to where ever happens to be a mine.  
 
It's completely self-contained and works pretty good. In the current form it solves only games from [Minesweeper X]
(http://www.minesweeper.info/downloads/MinesweeperX.html) with the "vistabluemineskin" skin. This is because it analyzes the screenshot pixel by pixel comparing the color of blocks with the ones in the skin. 
You can change it, though. CHnage the static color variables in Board.java. I even left the "debug" method in, which will print the correct color.
 
The solving process involves two solving strategies. 
 1. Neighbours, solve field by field 
 2. Multiple, for each case where more logic is required ([Tank solver by LuckyToile](https://luckytoilet.wordpress.com/2012/12/23/2125/))
 
Please keep in mind that I (or anyone else) can guarantee to solve a game. The nature of minesweeper doesn't allow for a fool proof strategy.

![example](https://raw.githubusercontent.com/arcs-/Minesweeper-Solver/master/resources/example.gif)