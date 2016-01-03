# Minesweeper-Solver 
A Minesweeper solver based on Java 
 
This is a solver to solve your minesweeper game, since it's so hard to do it yourself. It works by taking a screenshot, analyze it, then taking over the mouse and moving it to where ever happens to be a mine or not.  
 
It's completely self-contained and works pretty good. In the current form it solves only games from [Minesweeper X]
(http://www.minesweeper.info/downloads/MinesweeperX.html) with the "vistabluemineskin" skin. This is because it analyzes the screenshot pixel by pixel comparing the color of blocks with the ones in the skin. It's however possible to adjust this easily by changing the static color variables (most easily by using the "debug" method in board.java). 
 
The solving process involves two solving strategies. 
 1. Single   (if blocks around equal the number on the block -> open / else flag) 
 2. Multiple ([Tank solver by LuckyToile](https://luckytoilet.wordpress.com/2012/12/23/2125/))
 
Please keep in mind that you need luck to solve minesweeper since there is no 100% guaranteed strategy.. this includes this solver, it can't solve every game and will fail from time to time

![demo](http://stillhart.biz/project/minesweeper-solver/demo.gif)
