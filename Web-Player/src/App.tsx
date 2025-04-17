import type {Component} from 'solid-js';

import styles from './App.module.css';

const 
App: Component = (props) => {
  return (
    <div class={styles.App}>
      {props.children}
    </div>
  );
};

export default App;
