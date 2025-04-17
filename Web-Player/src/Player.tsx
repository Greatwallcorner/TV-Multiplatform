import {Component, createSignal, onMount, Show} from "solid-js";
import Player, {DefaultPreset, Events} from "xgplayer";
import HlsPlugin from "xgplayer-hls";
import "xgplayer/dist/index.min.css";
import {useSearchParams} from "@solidjs/router";
import Info from "./Info"


let component: Component = () => {
  let mse: HTMLDivElement | undefined;
  let [searchParams, setSearchParams] = useSearchParams();

  let [info, setInfo] = createSignal("")

  onMount(() => {
    if (mse) {
      let url = window.atob(searchParams.url as string)
      console.log("url: "+url);
      
      let player = new Player({
        id: mse.id,
        url: url,
        height: "100vh",
        width: "100%",
        autoplay: true,
        pip: true,
        cssFullscreen: false,
        plugins: [HlsPlugin],
        playsinline: true,
        presets: [DefaultPreset],
        hls: {
         fetchOptions:{
          mode: "cors"
         } 
        }
      });
      player.on(Events.ERROR, (err) => {
        console.error(err)
        console.log("play err: " + err);
        setInfo(`httpCode: ${err.httpCode} url: ${err.url} message: ${err.message}`)
      });
    }
  });
  return (
    <div>
      <Show when={info() != ""}>
        <Info info={info()}></Info>
      </Show>
      <div id="mse" ref={mse}></div>
    </div>
  );
};

export default component;
