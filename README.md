# Skynet Q-Learning Robocode Robot

A Robocode robot implementation using Q-learning for autonomous behavior.

## Structure

- `robot/` - Main robot implementation
- `qlearning/` - Q-learning algorithm implementation  
- `models/` - Opponent modeling and tracking
- `utils/` - State encoding and utility functions

## Build

```bash
mvn compile
mvn package
```

## Features

- Q-learning for movement decisions
- Q-learning for firing strategies
- Opponent modeling and tracking
- Modular design for multiple learning agents
- State encoding for efficient learning

## Future Optimizations

- Matrix operations for runtime efficiency
- Model compression for competition deployment
- Advanced state representation
- Multi-agent coordination
