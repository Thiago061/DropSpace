package com.dropspace;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    private final Videos videos;
    VideoController(Videos videos) { this.videos = videos; }

    @GetMapping
    public List<Video> list() { return videos.findAllByOrderByPositionAsc(); }
}
