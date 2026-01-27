-- Blog Sources Initialization
-- This file runs on every application startup with spring.sql.init.mode=always

INSERT IGNORE INTO blog_source (name, rss_url, is_active) VALUES
-- Korean Tech Company Blogs
('Kakao Tech', 'https://tech.kakao.com/feed/', true),
('Toss Tech', 'https://toss.tech/rss.xml', true),
('LINE Engineering', 'https://engineering.linecorp.com/ko/feed/', true),
('Woowa Bros', 'https://techblog.woowahan.com/feed/', true),
('Naver D2', 'https://d2.naver.com/d2.atom', true),
('Coupang Tech', 'https://medium.com/feed/coupang-engineering', true),
('Devsisters Tech', 'https://tech.devsisters.com/rss.xml', true),
('NHN Cloud', 'https://meetup.nhncloud.com/rss', true),
('Hyperconnect Tech', 'https://hyperconnect.github.io/feed.xml', true),
('Banksalad Tech', 'https://blog.banksalad.com/rss.xml', true),

-- Global Tech Company Blogs
('Netflix Tech', 'https://netflixtechblog.com/feed', true),
('Uber Engineering', 'https://www.uber.com/blog/engineering/rss/', true),
('Airbnb Tech', 'https://medium.com/feed/airbnb-engineering', true),
('Spotify Engineering', 'https://engineering.atspotify.com/feed/', true),
('GitHub Blog', 'https://github.blog/engineering.atom', true);
